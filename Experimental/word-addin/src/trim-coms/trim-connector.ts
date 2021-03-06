import Axios, { AxiosRequestConfig } from "axios";
import { BASE_URI } from "../stores/AppStore";
import { BaseObjectTypes } from "./trim-baseobjecttypes";
import TrimMessages from "./trim-messages";

export const SERVICEAPI_BASE_URI = BASE_URI + "ServiceAPI";

export interface ITrimString {
	Value: string;
}

interface IOptionsInterface {
	accessToken: string;
	path: string;
	method: string;
	data: any;
}

export interface IDriveInformation {
	Id: string;
	Uri: number;
}

export interface ITrimMainObject {
	Uri: number;
	NameString?: string;
}

export interface ILocation extends ITrimMainObject {
	FullFormattedName: ITrimString;
}

export interface IRecordType extends ITrimMainObject {}

export interface ITrimConnector {
	credentialsResolver: Promise<string>;
	getMe(): Promise<ILocation>;
	getMessages(): Promise<any>;
	search<T>(
		trimType: BaseObjectTypes,
		query: string,
		purpose: number
	): Promise<ITrimMainObject[]>;
	getPropertySheet(recordTypeUri: number): Promise<any>;
	registerInTrim(
		recordTypeUri: number,
		properties: any
	): Promise<ITrimMainObject>;
	getDriveId(webUrl: string): Promise<IDriveInformation>;
}

export class TrimConnector implements ITrimConnector {
	public credentialsResolver: Promise<string>;

	public getDriveId(webUrl: string): Promise<IDriveInformation> {
		return this.makeRequest(
			{ path: "RegisterFile", method: "get", data: { webUrl } },
			(data: any) => {
				return data.Results[0];
			}
		);
	}

	public registerInTrim(
		recordTypeUri: number,
		properties: any
	): Promise<ITrimMainObject> {
		const body = { ...properties, RecordRecordType: recordTypeUri };

		return this.makeRequest(
			{ path: "Record", method: "post", data: body },
			(data: any) => {
				return data.Results[0];
			}
		);
	}

	public getPropertySheet(recordTypeUri: number): Promise<any> {
		const params = {
			properties: "dataentryformdefinition",
		};
		return this.makeRequest(
			{ path: `RecordType/${recordTypeUri}`, method: "get", data: params },
			(data: any) => {
				return data.Results[0].DataEntryFormDefinition;
			}
		);
	}

	public getMessages(): Promise<any> {
		const params = {
			MatchMessages: Object.keys(new TrimMessages()).join("|"),
		};

		return this.makeRequest(
			{ path: "Localisation", method: "get", data: params },
			(data: any) => {
				return data.Messages;
			}
		);
	}

	public search<T extends ITrimMainObject>(
		trimType: BaseObjectTypes,
		q: string,
		purpose: number = 0
	): Promise<T[]> {
		const params = {
			properties: "NameString",
			purpose,
			q,
		};

		return this.makeRequest(
			{ path: trimType, method: "get", data: params },
			(data: any) => {
				return data.Results.map((trimObject: T) => {
					return trimObject;
				});
			}
		);
	}

	public getMe(): Promise<ILocation> {
		const params = {
			properties: ["LocationFullFormattedName"],
		};

		return this.makeRequest(
			{ path: "Location/me", method: "get", data: params },
			(data: any) => {
				return {
					FullFormattedName: data.Results[0].LocationFullFormattedName,
					Uri: data.Results[0].Uri,
				};
			}
		);
	}

	private makeOptions = (config: IOptionsInterface): AxiosRequestConfig => {
		const headers = { Accept: "application/json", Authorization: "" };

		if (config.accessToken) {
			headers.Authorization = `Bearer ${config.accessToken}`;
		}

		if (config.method === "post") {
			headers["Content-Type"] = "application/json";
		}

		const options = {
			headers,
			method: config.method,
			url: `${SERVICEAPI_BASE_URI}/${config.path}`,
		};

		if (config.method === "post") {
			return { ...options, ...{ data: config.data } };
		} else {
			return { ...options, ...{ params: config.data } };
		}
	};

	private makeRequest<T>(config: any, parseCallback: any): Promise<T> {
		return new Promise((resolve, reject) => {
			this.credentialsResolver.then((accessToken) => {
				const options = this.makeOptions({ ...{ accessToken }, ...config });

				Axios(options)
					.then((response) => {
						if (
							response.data.Messages ||
							(response.data.Results && response.data.Results.length > 0)
						) {
							resolve(parseCallback(response.data));
						} else {
							reject({ message: "No results found" }); // needs to come from TrimMessages
						}
					})
					.catch((error) => {
						if (error.response) {
							reject({
								message: error.response.data.ResponseStatus.Message,
							});
						} else {
							reject({
								message: error.message,
							});
						}
					});
			});
		});
	}
}

export default TrimConnector;
